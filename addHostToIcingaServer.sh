containers=$(docker ps -a | awk '{if(NR>1) print $NF}')

for container in $containers
do
  name=$(docker inspect --format="{{ .Name }}" $container)
    ip=$(docker inspect --format="{{ .NetworkSettings.IPAddress }}" $container)
    curl -u "root:icinga"  \
         -H 'Accept: application/json' -X PUT \
         -k "https://localhost:5665/v1/objects/hosts/$name" \
         -d '{ "templates": [ "generic-host" ], "attrs": {  "address": '\"$ip\"', "check_command": "hostalive", "vars.os" : "Linux" } }'
done
