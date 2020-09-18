import os
import asyncio
import functools

import asyncpg


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
    def __init__(self):
        self.conn = None
        self.loop = asyncio.get_event_loop()
        self.loop.create_task(self._initialize())

    async def _initialize(self):
        self.conn = await asyncpg.connect(os.getenv("POSTGRES_URL"))

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
        return data["build_label"]
