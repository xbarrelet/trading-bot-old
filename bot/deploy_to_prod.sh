EC2_URL=ubuntu@ec2-54-151-150-241.ap-southeast-1.compute.amazonaws.com

sbt assembly &&
scp -i ~/.ssh/trading-bot-prod-ec2.pem target/scala-3.1.2/trading-bot-assembly-0.1.0-SNAPSHOT.jar $EC2_URL:/var/bot &&
ssh -i ~/.ssh/trading-bot-prod-ec2.pem $EC2_URL "sudo /var/bot/kill_me.sh" && ssh -i ~/.ssh/trading-bot-prod-ec2.pem $EC2_URL 'sudo java -Dconfig.file=/var/bot/application.conf -jar /var/bot/trading-bot-assembly-0.1.0-SNAPSHOT.jar &'
