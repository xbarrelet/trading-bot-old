from pyrogram import Client


class SignalsClient:
    def __init__(self):
        self.api_id = 17219575
        self.api_hash = "ca261354cb45ea74ef1f19ccb1dd0b03"

    async def get_messages_with_string_in_group(self, search_term: str, chat_id: int) -> list[str]:
        async with Client("Mr X", self.api_id, self.api_hash) as async_app:
            filtered_messages: list[str] = []

            async for message in async_app.search_messages(chat_id, query=search_term, limit=2):
                print(message)
                filtered_messages.append(message.text)

            return filtered_messages
