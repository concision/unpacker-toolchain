from typing import Union

import re


class SemVer:
    update: str
    major: str
    minor: str
    hotfix: Union[str, type(None)]

    def __init__(self, update: str, major: str = "0", minor: str = "0", hotfix: str = None):
        self.update = update
        self.major = major
        self.minor = minor
        self.hotfix = hotfix

    def __str__(self):
        s = '.'.join(list(map(str, [self.update, self.major, self.minor])))
        return (s + '.' + str(self.hotfix)) if self.hotfix else s

    @classmethod
    def from_str(cls, version_string: str):
        matches = re.findall(r"(\d+(?:\.\d+)*)", version_string)
        return cls(*(matches[-1]).split('.'))
