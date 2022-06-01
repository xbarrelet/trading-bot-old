EC2_URL=ubuntu@ec2-3-0-148-172.ap-southeast-1.compute.amazonaws.com

sbt assembly &&
#scp -i ~/.ssh/trading-bot-ec2.pem kill_me.sh target/scala-3.1.2/trading-bot-assembly-0.1.0-SNAPSHOT.jar ../signals-listener/main.py ../signals-listener/Happy_Mr_X.session $EC2_URL:/var/bot &&
scp -i ~/.ssh/trading-bot-ec2.pem target/scala-3.1.2/trading-bot-assembly-0.1.0-SNAPSHOT.jar $EC2_URL:/var/bot &&
ssh -i ~/.ssh/trading-bot-ec2.pem $EC2_URL "sudo /var/bot/kill_me.sh" && ssh -i ~/.ssh/trading-bot-ec2.pem $EC2_URL 'sudo java -Dconfig.file=/var/bot/application.conf -jar /var/bot/trading-bot-assembly-0.1.0-SNAPSHOT.jar &'
