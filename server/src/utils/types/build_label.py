class BuildLabel:
    raw: str
    compile_time: str
    build_hash: str

    def __init__(self, build_label: str):
        self.raw = build_label
        self.compile_time, self.build_hash = self.raw.split('/')

    def __str__(self):
        return self.raw
