import asyncio
from contextlib import suppress

from src.utils import labels


class Tasker:
    def __init__(self):
        self.loop = asyncio.get_event_loop()
        self.values = {}
        self._running_tasks = {}

    def start_task(self, func, *args, **kwargs):
        """
        Create a task in the active event loop that calls func with args and kwargs
        If a task is running with the same name the old task will first be killed
        """
        name = func.__name__
        prev = self._running_tasks.get(name)

        if prev:
            self.loop.run_until_complete(self.task_kill(name))

        self._running_tasks.update({name: self.loop.create_task(func(*args, **kwargs))})

    async def task_kill(self, target: str):
        """Kills a task by func name and returns its result"""
        if target not in self._running_tasks:
            return None

        to_kill = self._running_tasks.pop(target)
        to_kill.cancel()

        with suppress(asyncio.CancelledError):
            return self.loop.run_until_complete(to_kill)

    async def shutdown(self):
        """Kills all tasks in _running_tasks"""
        for t in self._running_tasks:
            await self.task_kill(t)

    async def get_version(self, delay: int):
        """Task to get current wstate version and store in values dict. Repeats every `delay` seconds"""
        fetcher = labels.LabelFetcher()

        try:
            while True:
                latest = await fetcher.fetch_build_label()
                self.values.update({'current_version': latest})
                print({'current_version': latest})
                await asyncio.sleep(delay)

        except asyncio.CancelledError:
            return
