import os
import json
import asyncio
import functools
from hashlib import sha256
from datetime import datetime

import asyncpg
from fastapi import UploadFile

from .types import UpdateInfo
from .unpacker import Unpacker


def wait_until_ready(check):
    def wrapper(func):
        @functools.wraps(func)
        async def wrapped(*args, **kwargs):
            while not getattr(args[0], check, None):
                await asyncio.sleep(0)
            return await func(*args, **kwargs)
        return wrapped
    return wrapper


class Database:
    conn: asyncpg.Connection

    def __init__(self):
        self.loop = asyncio.get_event_loop()
        self.loop.create_task(self._initialize())

    async def _initialize(self):
        postgres_url = os.getenv("POSTGRES_URL")
        while not isinstance(getattr(self, "conn", None), asyncpg.Connection):
            try:
                self.conn = await asyncpg.connect(postgres_url)
            except asyncpg.exceptions.CannotConnectNowError:
                await asyncio.sleep(3)
                continue

    async def close(self):
        await self.conn.close()

    @wait_until_ready("conn")
    async def db_init(self):
        with open(os.path.join(os.path.dirname(__file__), "../sql/schematics.sql"), 'r') as schemas:
            await self.conn.execute(schemas.read())

    @wait_until_ready("conn")
    async def latest_version(self):
        data = await self.conn.fetchrow(
            "SELECT build_label FROM package_labels ORDER BY build_date_ordinal DESC LIMIT 1")
        return data["build_label"] if data is not None else None

    @wait_until_ready("conn")
    async def new_packages(self, packages: UploadFile, update_info: UpdateInfo):
        build_label = str(update_info.build_label)
        forum_version = str(update_info.semver) if update_info.semver else None
        forum_url = update_info.forum_link

        async with self.conn.transaction():
            await self.conn.execute(
                "INSERT INTO package_labels (timestamp, build_label, forum_version, forum_url) VALUES ($1, $2, $3, $4)",
                datetime.now(), build_label, forum_version, forum_url
            )
            try:
                async with self.conn.transaction():
                    with Unpacker() as unpacker:
                        async for package_record in unpacker.unpack(source=packages):
                            _hash = sha256(bytes(package := json.dumps(package_record["package"]), "utf-8")).digest()
                            await self.conn.execute(
                                "INSERT INTO package_blobs (sha256, contents) VALUES ($1, $2) ON CONFLICT DO NOTHING",
                                _hash, package
                            )
                            await self.conn.execute(
                                "INSERT INTO package_entries (build_date, path, sha256) VALUES ($1, $2, $3)",
                                update_info.build_label.build_date, package_record["path"], _hash
                            )
            finally:
                await self.conn.execute(
                    "INSERT INTO package_bins (build_date, packages) VALUES ($1, $2)",
                    update_info.build_label.build_date, unpacker.compressed_packages.getvalue()
                )
