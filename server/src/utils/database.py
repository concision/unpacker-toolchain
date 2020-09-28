import os
import json
import asyncio
from hashlib import sha256
from datetime import datetime

import asyncpg
from fastapi import UploadFile

from .types import UpdateInfo
from .unpacker import Unpacker


class Database:
    pool: asyncpg.pool.Pool

    async def initialize(self):
        postgres_dsn = os.getenv("POSTGRES_URL")
        while not isinstance(getattr(self, "pool", None), asyncpg.pool.Pool):
            try:
                self.pool = await asyncpg.create_pool(postgres_dsn)
            except asyncpg.exceptions.CannotConnectNowError:
                await asyncio.sleep(3)
                continue

    async def close(self):
        await self.pool.close()

    async def db_init(self):
        async with self.pool.acquire() as conn:
            with open(os.path.join(os.path.dirname(__file__), "../sql/schematics.sql"), 'r') as schemas:
                await conn.execute(schemas.read())

    async def latest_version(self):
        async with self.pool.acquire() as conn:
            data = await conn.fetchrow(
                "SELECT build_label FROM package_labels ORDER BY build_date_ordinal DESC LIMIT 1")
            return data["build_label"] if data is not None else None

    async def new_packages(self, packages: UploadFile, update_info: UpdateInfo):
        build_label = str(update_info.build_label)
        forum_version = str(update_info.semver) if update_info.semver else None
        forum_url = update_info.forum_link

        async with self.pool.acquire() as conn:
            async with conn.transaction():
                await conn.execute(
                    "INSERT INTO package_labels (timestamp, build_label, forum_version, forum_url) "
                    "VALUES ($1, $2, $3, $4)",
                    datetime.now(), build_label, forum_version, forum_url
                )
                try:
                    async with conn.transaction():
                        with Unpacker() as unpacker:
                            async for package_record in unpacker.unpack(source=packages):
                                _hash = sha256(
                                    bytes(package := json.dumps(package_record["package"]), "utf-8")
                                ).digest()
                                await conn.execute(
                                    "INSERT INTO package_blobs (sha256, contents) "
                                    "VALUES ($1, $2) ON CONFLICT DO NOTHING",
                                    _hash, package
                                )
                                await conn.execute(
                                    "INSERT INTO package_entries (build_date, path, sha256) VALUES ($1, $2, $3)",
                                    update_info.build_label.build_date, package_record["path"], _hash
                                )
                finally:
                    await conn.execute(
                        "INSERT INTO package_bins (build_date, packages) VALUES ($1, $2)",
                        update_info.build_label.build_date, unpacker.compressed_packages.getvalue()
                    )

    async def all_versions(self, fetch_all: bool = False):
        async with self.pool.acquire() as conn:
            return await conn.fetch(
                f"SELECT {'*' if fetch_all else 'build_label, forum_version, forum_url'} FROM package_labels"
            )
