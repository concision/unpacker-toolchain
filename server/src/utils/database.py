import os
import json
import asyncio
import functools
from hashlib import sha256
from datetime import datetime

import asyncpg

from .types import UpdateInfo


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
            "SELECT build_label FROM package_labels ORDER BY build_version_ordinal DESC LIMIT 1")
        return data["build_label"] if "build_label" in data else None

    @wait_until_ready("conn")
    async def new_packages(self, records: dict, packages: bytes, update_info: UpdateInfo):
        async with self.conn.transaction():
            await self.conn.execute(
                "INSERT INTO package_labels (timestamp, build_label, forum_version, forum_url) VALUES ($1, $2, $3, $4)",
                datetime.now(), str(update_info.build_label), str(update_info.semver), update_info.forum_link
            )
            await self.conn.execute(
                "INSERT INTO package_bins (build_version, packages) VALUES ($1, $2)",
                update_info.build_label.compile_time, packages
            )
            for key, value in records:
                _hash = sha256(bytes(value, "utf-8")).digest()
                is_in = bool(await self.conn.execute(
                    "SELECT COUNT(sha256) AS c FROM package_blobs WHERE sha256 = $1",
                    _hash
                ))
                if not is_in:
                    await self.conn.execute(
                        "INSERT INTO package_blobs (sha256, contents) VALUES ($1, $2)",
                        _hash, json.dumps(json.loads(value))
                    )
                await self.conn.execute(
                    "INSERT INTO package_entries (build_version, path, sha256) VALUES ($1, $2, $3)",
                    update_info.build_label.compile_time, key, _hash
                )
