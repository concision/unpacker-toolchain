from fastapi import FastAPI
from .utils.labels import LabelFetcher, UpdateVersion

app = FastAPI()
fetcher = LabelFetcher()


@app.get("/")
async def read_root():
    update_info = await fetcher.fetch_update_version()
    build_label = await fetcher.fetch_build_label()
    return {
        "update_version": str(UpdateVersion.from_str(update_info[0])),
        "build_label": build_label,
        "update_name": update_info[0],
        "patch_notes": update_info[1]
    }
