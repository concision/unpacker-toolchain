from typing import Union

from .semver import SemVer
from .build_label import BuildLabel


class UpdateInfo:
    semver: Union[SemVer, type(None)]
    build_label: BuildLabel
    forum_link: Union[str, type(None)]

    def __init__(self, semver: SemVer, build_label: BuildLabel, forum_link: str):
        self.semver = semver
        self.build_label = build_label
        self.forum_link = forum_link
