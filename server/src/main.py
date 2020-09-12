from typing import Optional

from fastapi import FastAPI, Query, Path

from src.utils.labels import LabelFetcher, UpdateVersion
from src.background.tasks import Tasker

fetcher = LabelFetcher()

app = FastAPI(
    title="Packages History API",
)


@app.on_event("startup")
async def startup_event():
    app.tasker = Tasker()
    app.tasker.start_task(app.tasker.get_version, 600)


@app.on_event("shutdown")
async def shutdown_event():
    _tasker = getattr(app, 'tasker')
    if _tasker:
        await _tasker.shutdown()


@app.get("/")
async def root():
    update_info = await fetcher.fetch_update_version()
    build_label = await fetcher.fetch_build_label()
    return {
        "update_version": str(UpdateVersion.from_str(update_info[0])),
        "build_label": build_label,
        "update_name": update_info[0],
        "patch_notes": update_info[1]
    }


@app.get("/versions")
async def versions(from_: Optional[str] = Query(None, alias="from"),
                   to: Optional[str] = Query(None),
                   count: Optional[int] = Query(None)):
    """
    With no parameters returns details about all available versions.

    If `from` parameter is specified returns details about versions after `from`.

    If `to` parameter is specified returns detail about before `to`.

    If `count` parameter is specified returns details about `count` latest versions.

    Parameters `from` and `to` can be specified simultaneously to return details about
    versions between the two, however, providing `count` in combination with either of the two
    results in HTTP 422.
    """
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

    If `relative` is set to True values of `first` and `second` are not sorted
    which allows for the changeset to be backwards.

    todo: `relative` is a terrible name
    """
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
    With no parameters returns details as well as package list from the specified `version`.

    If `patterns` are specified returns packages paths that match the any of the patterns.

    If `full` is set to True it will instead return entire packages that match provided `patterns`,
    if `full` is set but no `patterns` are provided results in HTTP 422
    """
    ...


@app.get("/packages")
async def packages(package: str = Query(...),
                   _from: Optional[str] = Query(None, alias='from'),
                   to: Optional[str] = Query(None),
                   count: Optional[int] = Query(None)):
    """
    Fetches historical data about a `package`.

    By default fetches the full package on the latest version.

    If `from` and `to` are specified it will fetch the package on all versions between the two.

    If `count` is specified it will fetch the package on `count` amount of latest versions.

    Providing either of `from` or `to` without the other results in HTTP 422.

    Providing `from` and `to` as well as `count` results in HTTP 422.
    """
    ...
