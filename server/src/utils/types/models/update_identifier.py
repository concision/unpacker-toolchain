from typing import Optional

from pydantic import BaseModel, root_validator

from .. import SemVer, BuildLabel


class UpdateIdentifier(BaseModel):
    semver: Optional[SemVer]
    buildlabel: Optional[BuildLabel]

    @root_validator
    def exactly_one_identifier(cls, values):
        if not sum([1 if value else 0 for _, value in values]) == 1:
            raise ValueError("Exactly one update identifier must be provided")
