import psycopg
from psycopg.rows import dict_row


class SignalsRepository:
    def __init__(self):
        self.db_connection = psycopg.connect(
            'postgresql://root:toor@localhost:5429/data', row_factory=dict_row, autocommit=True)

    def insert_signal(self, signal: dict) -> None:
        with self.db_connection.cursor() as cursor:
            cursor.execute("""INSERT INTO public.signals 
            (entry_price, first_target_price, second_target_price, third_target_price, is_long, stop_loss, symbol, 
            "timestamp") VALUES (%s, %s, %s, %s, %s, %s, %s, %s)""",
                           (signal['entryPrice'], signal['firstTargetPrice'], signal['secondTargetPrice'],
                            signal['thirdTargetPrice'], signal['firstTargetPrice'] > signal['entryPrice'],
                            signal['stopLoss'], signal['symbol'], signal['timestamp']))
