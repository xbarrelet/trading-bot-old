from pyrogram import Client


class SignalsListener:
    def __init__(self):
        self.api_id = 17219575
        self.api_hash = "ca261354cb45ea74ef1f19ccb1dd0b03"
        self.app = Client("Mr X", self.api_id, self.api_hash)

