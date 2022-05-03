import psycopg
from psycopg.rows import dict_row


class SignalsRepository:
    def __init__(self):
        self.db_connection = psycopg.connect(
            'postgresql://root:toor@localhost:5430/data', row_factory=dict_row, autocommit=True)

    def insert_signal(self, signal: dict) -> None:
        if 'entryPrice' in signal and 'firstTargetPrice' in signal and 'stopLoss' in signal and 'symbol' in signal:
            with self.db_connection.cursor() as cursor:
                cursor.execute("""INSERT INTO public.signals 
                (entry_price, first_target_price, is_long, stop_loss, symbol, "timestamp")
                VALUES(%s, %s, %s, %s, %s, %s)""", (signal['entryPrice'], signal['firstTargetPrice'],
                                                    signal['firstTargetPrice'] > signal['entryPrice'],
                                                    signal['stopLoss'], signal['symbol'], signal['timestamp']))
