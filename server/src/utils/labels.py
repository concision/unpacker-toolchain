import json
from xml.etree import ElementTree

from bs4 import BeautifulSoup

from .http import Route, HTTPClient
from .sanitization import desanitize


class LabelFetcher:
    WSTATE = desanitize("aHR0cDovL29yaWdpbi53YXJmcmFtZS5jb20vZHluYW1pYy93b3JsZFN0YXRlLnBocA==")
    FORUMS = desanitize("aHR0cHM6Ly9mb3J1bXMud2FyZnJhbWUuY29tL2ZvcnVtLzMtcGMtdXBkYXRlLW5vdGVzLw==")
    RSS = FORUMS + ".xml"

    _client: HTTPClient

    def __init__(self):
        self._client = HTTPClient()

    async def close(self):
        await self._client.close()

    async def fetch_build_label(self) -> str:
        r = json.loads(await self._client.request(Route("GET", self.WSTATE)))
        if "BuildLabel" not in r:
            raise ValueError
        return r["BuildLabel"]

    async def fetch_update_version(self, rss: bool = True) -> list[str]:
        r = await self._client.request(Route("GET", (self.RSS if rss else self.FORUMS)))

        if rss:
            tree = ElementTree.fromstring(r)
            thread = list(tree.iter("item"))[0]
            return [list(thread.iter("title"))[0].text, list(thread.iter("link"))[0].text]
        else:
            soup = BeautifulSoup(r, features="html.parser")
            thread = (soup.find(class_="ipsDataItem ipsDataItem_responsivePhoto")
                      .find(class_="ipsType_break ipsContained")
                      .a)
            return [thread["title"].strip(), thread["href"]]
