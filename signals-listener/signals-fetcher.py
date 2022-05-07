import asyncio
import re
import time

import requests

from signals.SignalsClient import SignalsClient
from signals.SignalsRepository import SignalsRepository


def does_contain_number(line: str) -> bool:
    return any(char.isdigit() for char in line)


def extract_number_from_string(string: str) -> float:
    if any(map(str.isdigit, string)):
        if string.lstrip().startswith("."):
            string = '0' + string.lstrip()

        if '.' in string:
            return float(re.findall("\d+\.\d+", string)[0])
        else:
            return float(re.findall("\d+", string)[0])


# async def main():
#     async with Client("Happy_Mr_X", 12777340, "106f07a83293f2065a80cc5ab3f458ff") as app:
#         invite_link: str = "https://t.me/+fjRGR_9nLlc3NWNl"
#
#         chat: Chat = await app.get_chat(invite_link)
#         print(chat.id)

def get_symbols_supported_by_bybit() -> list[str]:
    answer = requests.get("https://api.bybit.com/v2/public/symbols").json()
    results = answer['result']
    symbols: list[str] = []

    for result in results:
        name: str = result['name']
        if name.endswith('USDT'):
            symbols.append(name.split("USDT")[0])

    return symbols


if __name__ == '__main__':
    alt_chica_chat_id = -1001379320382
    alt_chica_signal_search_term = 'Target 3'

    repository: SignalsRepository = SignalsRepository()
    signal_client = SignalsClient()

    allowed_symbols: list[str] = get_symbols_supported_by_bybit()
    allowed_signals_counter = 0
    forbidden_signals_counter = 0
    ignored_symbols: set = set()

    messages = asyncio.run(signal_client.get_messages_with_string_in_group(search_term=alt_chica_signal_search_term,
                                                                           chat_id=alt_chica_chat_id))
    for message in messages:
        signal_text = message.caption

        if signal_text is not None:
            signal: dict = {"timestamp": time.mktime(message.date.timetuple())}

            split_lines_in_text: list[str] = signal_text.split("\n")
            for line in split_lines_in_text:
                if 'VIP:' in line:
                    if '/' in line:
                        signal['symbol'] = line.split("/")[0]
                    else:
                        signal['symbol'] = line.split("VIP")[0].rstrip()

                elif 'entry:' in line.lower() and does_contain_number(line):
                    prices_text: str = line.lower().split('entry:')[1]
                    if "-" in prices_text:
                        signal["entryPrice"] = extract_number_from_string(prices_text.split("-")[1])
                    else:
                        signal["entryPrice"] = extract_number_from_string(prices_text)

                elif 'S/L' in line and does_contain_number(line):
                    signal["stopLoss"] = extract_number_from_string(line.split("S/L:")[1])
                elif 'Target 1:' in line and does_contain_number(line):
                    signal["firstTargetPrice"] = extract_number_from_string(line.split("Target 1:")[1])
                elif 'Target 2:' in line and does_contain_number(line):
                    signal["secondTargetPrice"] = extract_number_from_string(line.split("Target 2:")[1])
                elif 'Target 3:' in line and does_contain_number(line):
                    signal["thirdTargetPrice"] = extract_number_from_string(line.split("Target 3:")[1])

            if all(field in signal for field in ('symbol', "entryPrice", "stopLoss", "firstTargetPrice",
                                                 "secondTargetPrice", "thirdTargetPrice")):
                if signal['symbol'] in allowed_symbols:
                    repository.insert_signal(signal)
                    allowed_signals_counter = allowed_signals_counter + 1
                else:
                    forbidden_signals_counter = forbidden_signals_counter + 1
                    ignored_symbols.add(signal['symbol'])

    print(f'signals inserted:{allowed_signals_counter}, signals ignored:{forbidden_signals_counter} with '
          f'currencies:{ignored_symbols}')
