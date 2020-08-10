# AutoML from development to production

Prerequisite:
- Docker
- Jupyter Notebook
- Python and Pip
- Java

In recent years, the demand for data scientist and analyst expert has outpaced the supply, despite the surge of
the people entering the field. To answer this gap, we need some friendlies machine learning frameworks that can be used
by non-experts user. 

Although some machine learning framework like Tensorflow, H2O has made it easy for non-experts to experiment with 
machine learning, there is still a fair bit of knowledge and background in data science that is required to produce 
high-performing machine learning models. As we want to remove this gap I would like to introduce some quite good, and 
a new concept called AutoML.

AutoML is an idea to automate the machine learning workflow, which includes automatic training and tuning 
of many models within a user-specified time-limit. It will be automatically trained on collections of individual models 
to produce highly predictive ensemble models which, in most cases, will be the top performing models in the AutoML 
Leaderboard.

One of the available framework to achieve this purpose is H2O, it's possible for non-AI users, and it's also a friendly
framework for the developer who didn't have any previous experience to analyzing or developing a model. Before we move 
further, I want to define the goal first. The main goal is to train the model in the most common way for training a 
model in python language using jupyter notebook, and then for the software engineer part we want to deliver the model
in the most used language for the large scale applications with `java` language in the production. I have already
provide ready to used example for you to be convenience to follow this guide.

First of all, we need to run the H2O in the docker that I already prepared for you, at the first line we want to make
sure there is no docker container named `h2o` already exists in your computer / laptop. The next line is for running
the docker container and open some ports to be available on your local machine.
```
docker container rm h2o
docker run -ti --name=h2o -p 54321:54321 -p 8888:8888 adrian3ka/h2o:0.0.1 /bin/bash
```

Don't expect anything yet, because we only the entering the docker machine and didn't run anything yet.
The command below will be launch H2O applications:
```
cd /opt
java -Xmx1g -jar h2o.jar
```

I just wanted to introduce that we also could develop the model via H2O-Flow notebook on website view, but we will use
jupyter notebook as it is the most used tools today. You could access it through the localhost:54321 as we already exposed
the port from the command above `-p 54321:54321`.

Now we will try to launch the jupyter notebook, first we need to launch another docker terminal:
```
export H2O_CONTAINER_ID=$(docker ps -aqf "name=h2o")
docker exec -it $H2O_CONTAINER_ID bash
```

Inside the docker terminal, we would try to initiate the jupyter notebook, but we should get the docker IP information
by running some command that already explained below. Here are the command:
```
cd ~/h2o # we would try to move to the h2o folder that I already prepared for this example
virtualenv h2o_venv

source h2o_venv/bin/activate

# open new terminal to check the docker local docker IP
export H2O_CONTAINER_ID=$(docker ps -aqf "name=h2o")
docker inspect $H2O_CONTAINER_ID | grep "\"IPAddress\"" -m1

# use ip from the output, my output are: 172.17.0.3

jupyter notebook --ip=172.17.0.3 --port=8888 --allow-root
```

As for now you could see there will be link and token provided on the terminal, we could access it on the 
web browser. Although we could run it on the web browser, but we didn't have any code to be run on the notebook.
So before we move to the main part, training the model from jupyter notebook we need to open a new terminal 
to copy the example to the located folder on the `/root/h2o/h2o_venv` inside the docker:
```
export H2O_CONTAINER_ID=$(docker ps -aqf "name=h2o")

docker cp automl_binary_classification.ipynb $H2O_CONTAINER_ID:/root/h2o/h2o_venv/automl_binary_classification.ipynb
docker cp product_backorders.csv $H2O_CONTAINER_ID:/root/h2o/h2o_venv/product_backorders.csv
```

From there you could see and run it 1 by 1. I would like to highlight some important part on the jupyter
notebook.

Open the jupyter notebook from the website based on the information from the terminal

Build and pass the copy the package to docker:
```
cd model-predictor

export H2O_CONTAINER_ID=$(docker ps -aqf "name=h2o")
docker cp . $H2O_CONTAINER_ID:/root/h2o/h2o_venv/model-predictor
```

Inside the docker:
```
export H2O_CONTAINER_ID=$(docker ps -aqf "name=h2o")

docker exec -it $H2O_CONTAINER_ID bash

cd ~/h2o/h2o_venv/model-predictor
mvn package
mvn exec:java -Dexec.mainClass="com.example.Main"
```
Reference:
- https://docs.h2o.ai/h2o/latest-stable/h2o-docs/automl.html accessed at 6th August 2020.

---
##### Note to self:
```
docker build -t "adrian3ka/h2o:0.0.1" .

docker tag adrian3ka/h2o:0.0.1 adrian3ka/h2o:0.0.1
docker push adrian3ka/h2o:0.0.1
```
