from pyrogram import Client


class SignalsClient:
    def __init__(self):
        self.api_id = 12777340
        self.api_hash = "106f07a83293f2065a80cc5ab3f458ff"

    async def get_messages_with_string_in_group(self, search_term: str, chat_id: int) -> list[str]:
        async with Client("Happy_Mr_X", self.api_id, self.api_hash) as async_app:
            filtered_messages: list[str] = []

            async for message in async_app.search_messages(chat_id, query=search_term, limit=10000):
                filtered_messages.append(message)

            return filtered_messages
