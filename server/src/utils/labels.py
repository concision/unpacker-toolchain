from typing import Union

import re
import json
from xml.etree import ElementTree
from base64 import b64decode

from bs4 import BeautifulSoup

from .http import Route, HTTPClient


class UpdateVersion:
    update: str
    major: str
    minor: str
    hotfix: Union[str, type(None)]

    def __init__(self, update: str, major: str, minor: str, hotfix=None):
        self.update = update
        self.major = major
        self.minor = minor
        self.hotfix = hotfix

    def __str__(self):
        s = '.'.join(list(map(str, [self.update, self.major, self.minor])))
        return (s + '.' + str(self.hotfix)) if self.hotfix else s

    @classmethod
    def from_version_str(cls, version_string: str):
        matches = re.findall(r"(\d+(?:\.\d+)*)", version_string)
        return cls(*(matches[-1]).split('.'))


class LabelFetcher:
    WSTATE = b64decode("aHR0cDovL29yaWdpbi53YXJmcmFtZS5jb20vZHluYW1pYy93b3JsZFN0YXRlLnBocA==").decode()
    FORUMS = b64decode("aHR0cHM6Ly9mb3J1bXMud2FyZnJhbWUuY29tL2ZvcnVtLzMtcGMtdXBkYXRlLW5vdGVzLw==").decode()
    RSS = FORUMS + ".xml"

    _client = HTTPClient()

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
