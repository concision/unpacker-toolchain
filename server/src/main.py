from typing import Optional

from fastapi import FastAPI, Query, Path, HTTPException

from src.utils.labels import LabelFetcher, UpdateVersion
from src.background.tasks import Tasker
from src.utils.database import Database


class Globals:
    def __init__(self):
        self.tasker = Tasker()
        self.fetcher = LabelFetcher()
        self.db = Database()

    async def shutdown(self):
        await self.tasker.shutdown()
        await self.fetcher.close()
        await self.db.close()


app = FastAPI(
    title="Packages History API",
)

_globals = Globals()


@app.on_event("startup")
async def startup_event():
    await _globals.db.db_init()
    _globals.tasker.values['current_version'] = await _globals.db.latest_version()
    await _globals.tasker.start_task(_globals.tasker.get_version, 600, fetcher=_globals.fetcher)


@app.on_event("shutdown")
async def shutdown_event():
    await _globals.shutdown()


@app.get("/")
async def root():
    update_info = await _globals.fetcher.fetch_update_version()
    build_label = await _globals.fetcher.fetch_build_label()
    return {
        "update_version": str(UpdateVersion.from_str(update_info[0])),
        "build_label": build_label,
        "update_name": update_info[0],
        "patch_notes": update_info[1]
    }


@app.get("/versions")
async def versions(_from: Optional[str] = Query(None, alias="from"),
                   to: Optional[str] = Query(None),
                   count: Optional[int] = Query(None)):
    """
    With no parameters returns details about all available versions.

    If `from` parameter is specified, returns details about versions after `from`.

    If `to` parameter is specified, returns details about before `to`.

    If `count` parameter is specified, returns details about `count` latest versions.

    Parameters `from` and `to` can be specified simultaneously to return details about
    versions between the two.

    Providing both `from` and `to` as well as `count` results in HTTP 422.
    """
    if all((_from, to, count)):
        raise HTTPException(
            status_code=422,
            detail="Parameters from, to and count cannot all be set"
        )
    ...


@app.get("/versions/latest")
async def versions_latest():
    """
    Returns details about the latest version.
    """
    ...


@app.get("/versions/oldest")
async def versions_oldest():
    """
    Returns details about the oldest version.
    """
    ...


@app.get("/versions/diff")
async def versions_diff(first: str = Query(...),
                        second: str = Query(...),
                        relative: Optional[bool] = Query(False)):
    """
    Returns a changeset of all packages between two versions.

    If `relative` is set to True, values of `first` and `second` are not sorted
    which allows for the changeset to be backwards.
    """
    # todo: `relative` is a terrible name
    ...


@app.get("/versions/{version}")
async def versions_specific(version: str = Path(...)):
    """
    Returns details about the specified `version`.
    """
    ...


@app.get("/versions/{version}/Packages.bin")
async def versions_packages_bin(version: str = Path(...)):
    """
    Download packages.bin from a specified `version`.
    """
    ...


@app.get("/versions/{version}/packages")
async def versions_packages(version: str = Path(...),
                            patterns: Optional[list[str]] = Query([]),
                            full: Optional[bool] = Query(False)):
    """
    With no parameters, returns details as well as package list from the specified `version`.

    If `patterns` are specified, returns packages paths that match the any of the patterns.

    If `full` is set to True, it will instead return entire packages that match provided `patterns`,
    setting `full` without providing `patterns` results in HTTP 422.
    """
    if full and not patterns:
        raise HTTPException(
            status_code=422,
            detail="Patterns need to be provided when requesting full packages"
        )
    ...


@app.get("/packages")
async def packages(package: str = Query(...),
                   _from: Optional[str] = Query(None, alias='from'),
                   to: Optional[str] = Query(None),
                   count: Optional[int] = Query(None)):
    """
    Fetches historical data about a `package`.

    By default fetches the full package on the latest version.

    If `from` is specified, returns all versions of the package from that version.

    If `to` is specified, returns all versions of the package up to that version.

    If `from` and `to` are specified, returns all versions of the package between the two.

    If `count` is specified, it will fetch the package on `count` amount of latest versions.

    Providing both `from` and `to` as well as `count` results in HTTP 422.
    """
    if all((_from, to, count)):
        raise HTTPException(
            status_code=422,
            detail="Parameters from, to and count cannot all be set")
    ...
