import asyncio
import logging
import requests

from pyrogram import Client, types


def instantiate_logging():
    logging.basicConfig(format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
                        level=logging.INFO)


if __name__ == '__main__':
    instantiate_logging()

    crypto_signals_chat_id = -1001199352806
    crypto_signals_signal_search_term = 'Signal'

    test_chat_id = -697780299

    # You can use this one to get all past signals and save them in a new table in db for backtesting :)
    # signal_client = SignalsClient()
    # asyncio.run(signal_client.get_messages_with_string_in_group(search_term=crypto_signals_signal_search_term,
    #                                                             chat_id=crypto_signals_chat_id))

    app = Client("Mr X", 17219575, "ca261354cb45ea74ef1f19ccb1dd0b03")


    @app.on_message()
    async def my_handler(client, message):
        message: types.Message = message
        if message.chat.id == crypto_signals_chat_id and crypto_signals_signal_search_term in message.text:
            signal_text = message.text
            signal: dict = {
                "entryPrice": float(signal_text.split("Entry price: $")[1].split("\n")[0]),
                "firstTargetPrice": float(signal_text.split("Target: $")[1].split("\n")[0]),
                "stopLoss": float(signal_text.split("Stop: $")[1].split("\n")[0]),
                "symbol": signal_text.split("Instrument: ")[1].split("/USD")[0],
                "isLong": "Buy" in signal_text.split("My opinion:")[1].split("\n")[0],
            }
            logging.info(f"Received signal from crypto_signals:{signal}")
            requests.post('http://localhost:8080/signal', json=signal)

    app.run()
