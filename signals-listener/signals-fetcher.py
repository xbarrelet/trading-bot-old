import asyncio
import re
import time

from signals.SignalsClient import SignalsClient
from signals.SignalsRepository import SignalsRepository


def does_contain_number(line: str) -> bool:
    return any(char.isdigit() for char in line)


if __name__ == '__main__':
    crypto_signals_chat_id = -1001199352806
    crypto_signals_signal_search_term = 'Signal'

    repository: SignalsRepository = SignalsRepository()
    signal_client = SignalsClient()
    messages = asyncio.run(
        signal_client.get_messages_with_string_in_group(search_term=crypto_signals_signal_search_term,
                                                        chat_id=crypto_signals_chat_id))
    for message in messages:
        signal_text = message.text

        if signal_text is not None:
            signal: dict = {"timestamp": time.mktime(message.date.timetuple())}

            split_lines_in_text: list[str] = signal_text.split("\n")
            for line in split_lines_in_text:
                if 'instrument' in line.lower():
                    signal['symbol'] = signal_text.split('Instrument: ')[1].split("/USD")[0]
                elif 'entry' in line.lower() and does_contain_number(line):
                    signal["entryPrice"] = float(re.findall(r'\d+', line)[0])
                elif 'stop' in line.lower() and does_contain_number(line):
                    signal["stopLoss"] = float(re.findall(r'\d+', line)[0])
                elif 'target' in line.lower() and does_contain_number(line):
                    signal["firstTargetPrice"] = float(re.findall(r'\d+', line)[0])

            if signal['symbol'] not in ('ETN', 'SHIB', 'QST', 'QSP', 'LBLOCK', 'BTN', 'OXY', 'BNT', 'UMA'):
                repository.insert_signal(signal)
