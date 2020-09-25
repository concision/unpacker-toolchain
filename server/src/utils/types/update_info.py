from typing import Union, Optional

from .semver import SemVer
from .build_label import BuildLabel


class UpdateInfo:
    semver: Union[SemVer, type(None)]
    build_label: BuildLabel
    forum_link: Union[str, type(None)]

    def __init__(self, build_label: BuildLabel, semver: Optional[SemVer] = None, forum_link: Optional[str] = None):
        self.semver = semver
        self.build_label = build_label
        self.forum_link = forum_link
