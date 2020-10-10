from typing import Union, Optional

from pydantic import HttpUrl

from .semver import SemVer
from .build_label import BuildLabel


class UpdateInfo:
    semver: Union[SemVer, type(None)]
    build_label: BuildLabel
    forum_link: Union[HttpUrl, type(None)]

    def __init__(self, build_label: BuildLabel, semver: Optional[SemVer] = None, forum_link: Optional[HttpUrl] = None):
        self.semver = semver
        self.build_label = build_label
        self.forum_link = forum_link
