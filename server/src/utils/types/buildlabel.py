from typing import Optional

import re
from functools import reduce


_buildlabel_pattern = re.compile(r"\d{4}(?:\.\d{2}){4}/\S*")


class BuildLabel(str):
    raw: str
    build_date: str
    build_hash: Optional[str]

    def __init__(self, buildlabel: str):
        str.__init__(buildlabel)
        if '/' in buildlabel:
            self.build_date, self.build_hash = self.raw.split('/')
        else:
            self.build_date = buildlabel

    def __str__(self):
        return self.raw

    def to_ordinal(self):
        return reduce(int.__or__, [v << (48 - i*12) for i, v in enumerate(list(map(int, self.build_date.split('.'))))])

    @classmethod
    def __get_validators__(cls):
        yield cls.validate

    @classmethod
    def validate(cls, v: str):
        if not isinstance(v, str):
            raise TypeError("String required, got {!r}".format(v.__class__))
        if not _buildlabel_pattern.fullmatch(v):
            raise ValueError("Invalid BuildLabel format")
        return cls(v)
