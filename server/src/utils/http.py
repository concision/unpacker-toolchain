import json
import asyncio

import aiohttp
from urllib.parse import quote as _uriquote


async def json_or_text(response: aiohttp.request):
    text = await response.text(encoding='utf-8')
    return json.loads(text) if response.headers.get('content-type') == 'application/json' else text


class Route:
    def __init__(self, method: str, url: str, **parameters):
        self.method = method
        if parameters:
            self.url = url.format(**{k: _uriquote(v) if isinstance(v, str) else v for k, v in parameters.items()})
        else:
            self.url = url


class HTTPClient:
    """
    Low level http client responsible for making requests.
    """
    __session: aiohttp.ClientSession

    def __init__(self):
        self.__session = aiohttp.ClientSession()

    async def request(self, route: Route, **kwargs):
        method = route.method
        url = route.url

        for tries in range(5):
            try:
                async with self.__session.request(method, url, **kwargs) as r:
                    data = await json_or_text(r)

                    if 300 > r.status >= 200:
                        return data

                    if r.status == 429:
                        await asyncio.sleep(10)
                        continue

                    if r.status in {500, 502}:
                        await asyncio.sleep(2 * tries + 1)
                        continue

                    raise aiohttp.ClientResponseError(r.request_info, r.history, status=r.status)

            except OSError as e:
                if tries < 4 and e.errno in (54, 10054):
                    continue
                raise
        raise aiohttp.ClientResponseError(r.request_info, r.history, status=r.status)
