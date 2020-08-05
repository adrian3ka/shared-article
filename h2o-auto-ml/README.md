docker container rm h2o
docker run -ti --name=h2o -p 54321:54321 -p 8888:8888 adrian3ka/h2o:0.0.1 /bin/bash

Launch H2O:
```
cd /opt
java -Xmx1g -jar h2o.jar
```

Launch another docker terminal:
```
export H2O_CONTAINER_ID=$(docker ps -aqf "name=h2o")

docker exec -it $H2O_CONTAINER_ID bash
```

In docker terminal:
```
cd ~/h2o

virtualenv h2o_venv

source h2o_venv/bin/activate

docker inspect $H2O_CONTAINER_ID | grep "\"IPAddress\"" -m1

# use ip from the output, my output are: 172.17.0.3

jupyter notebook --ip=172.17.0.3 --port=8888 --allow-root
```

Open a new terminal to copy the example to the located folder:
```
export H2O_CONTAINER_ID=$(docker ps -aqf "name=h2o")

docker cp automl_binary_classification.ipynb $H2O_CONTAINER_ID:/root/h2o/h2o_venv/automl_binary_classification.ipynb
```

Note to self:
```
docker build -t "adrian3ka/h2o:0.0.1" .

docker tag adrian3ka/h2o:0.0.1 adrian3ka/h2o:0.0.1
docker push adrian3ka/h2o:0.0.1
```


