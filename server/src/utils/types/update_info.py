from typing import Union, Optional

from pydantic import HttpUrl

from .semver import SemVer
from .buildlabel import BuildLabel


class UpdateInfo:
    semver: Union[SemVer, type(None)]
    buildlabel: BuildLabel
    forum_link: Union[HttpUrl, type(None)]

    def __init__(self, buildlabel: BuildLabel, semver: Optional[SemVer] = None, forum_link: Optional[HttpUrl] = None):
        self.semver = semver
        self.buildlabel = buildlabel
        self.forum_link = forum_link
