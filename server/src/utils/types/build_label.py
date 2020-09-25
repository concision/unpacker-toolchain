from typing import Optional


class BuildLabel:
    raw: str
    build_date: str
    build_hash: Optional[str]

    def __init__(self, build_label: str):
        self.raw = build_label
        if '/' in build_label:
            self.build_date, self.build_hash = self.raw.split('/')
        else:
            self.build_date = build_label

    def __str__(self):
        return self.raw
