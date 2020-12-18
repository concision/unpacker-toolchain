from typing import Union

import re
from functools import reduce


_semver_pattern = re.compile(r"(\d+(?:\.\d+){0,3})")


class SemVer(str):
    update: str
    major: str
    minor: str
    hotfix: Union[str, type(None)]

    def __init__(self, update: str, major: str = "0", minor: str = "0", hotfix: str = None):
        raw = [update, major, minor]
        if hotfix:
            raw.append(hotfix)
        str.__init__('.'.join(raw))
        self.update = update
        self.major = major
        self.minor = minor
        self.hotfix = hotfix

    def to_ordinal(self):
        _ = list(map(int, [self.update, self.major, self.minor, self.hotfix if self.hotfix is not None else 0]))
        return reduce(int.__or__, [v << (48 - i*12) for i, v in enumerate(_)])

    @classmethod
    def from_str(cls, version_string: str):
        matches = _semver_pattern.findall(version_string)
        return cls(*(matches[-1]).split('.'))

    @classmethod
    def __get_validators__(cls):
        yield cls.validate

    @classmethod
    def validate(cls, v: str):
        """Makes sure received data is in proper format and returns an instance"""
        if not isinstance(v, str):
            raise TypeError("String required, got {!r}".format(v.__class__))
        if not _semver_pattern.findall(v):
            raise ValueError("Invalid SemVer format")
        return cls.from_str(v)
