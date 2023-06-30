build:
	mvn clean install -B -U
start-kafka:
	docker-compose up -d
check-kafka:
	nc -z localhost 2181
	nc -z localhost 9092
	#For Windows nc -> ncat
	ncat -z localhost 2181
	ncat -z localhost 9092
	docker-compose logs kafka | grep -i 'started'
stop-kafka:
	docker-compose down --remove-orphans
