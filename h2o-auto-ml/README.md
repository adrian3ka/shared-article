# AutoML from development to production

Prerequisite:
- Docker
- Jupyter Notebook
- Python and Pip
- Java
- Maven
- Lombok

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
model in `python` language using jupyter notebook, and then for the software engineer part we want to deliver the model
in the most used language for the large scale applications with `java` language in the production. I have already
provide ready to used example for you to be convenience to follow this guide. I choose `java` over any other languages
because it one of the popular language on the industry, and it would be very relevant to the real world problem.

First of all, we need to run the H2O in the docker that I already prepared for you, at the first line we want to make
sure there is no docker container named `h2o` already exists in your computer / laptop. The next line is for running
the docker container and open some ports to be available on your local machine.
```
docker container rm h2o
docker run -ti --name=h2o -p 54321:54321 -p 8888:8888 adrian3ka/h2o:0.0.1 /bin/bash
```

Don't expect anything yet, because we only the entering the docker machine and didn't run anything yet.
The command below will be launch H2O applications by running the jar directly:
```
cd /opt
java -Xmx1g -jar h2o.jar
```

I just wanted to let you know that we also could develop the model via H2O-Flow notebook on website view, but we will 
use jupyter notebook as it is the most used tools today. You could access `H2O-Flow` through the localhost:54321 as we 
already exposed the port from the command above `-p 54321:54321`.

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
Now, open the jupyter notebook from the website based on the information from the terminal based on the link you see
earlier from the docker terminal. After that try to click the folder called `h20_venv` and then select 
`automl_binary_classification.ipynb` try to double-click it. From there you could see the code, and you should run it 
1 by 1. I would like to highlight some important part on the jupyter notebook.

```
import h2o
import pandas
from h2o.automl import H2OAutoML
h2o.init()
```
It would try to check if any h2o instance running, if its not it will try to run the h2o. It would produce the h2o
instance information about the uptime, timezone, version, etc.

```
aml = H2OAutoML(max_models = 10, seed = 1)
aml.train(x = x, y = y, training_frame = df)
```
The next important part is when we try to train the data using the specified parameters. First `max_models` is an
argument that specified the number of individual models to be trained for. If you make it the number bigger it would
a wide range to train many models to get the optimum one, but if you lower it you could achieve the training time. Be 
wise to make use of it. The `train` method will try to train the model with the available algorithm already provided by 
h2o framework (it will be chosen by default by the system).

```
%matplotlib inline
metalearner.std_coef_plot()
```
It will display the models' effectiveness based on their score and will be very helpful to read. 

```                                                                                                 
h2o.save_model(aml.leader, path = "./product_backorders_model_bin")
aml.leader.download_mojo(path = "./top_model.zip")
```

There are two ways to save the leader model those are the binary format and MOJO format. If you're taking your leader 
model to production, then I would suggest the MOJO format since it's optimized for production use. We should define the
path including the file name on the `path` parameter for any method that we want to use.

```
validation_df[validation_df["went_on_backorder"] == "Yes"].head()
validation_df[validation_df["went_on_backorder"] == "No"].head()
preds = aml.predict(test_frame)
preds.head()
```

Before we proceed to deploying the model we should check whether our model properly trained or not, `test_frame`
variable contains the data came from the part of the data set to check whether our model could predict correctly.
If you follow the guide on the notebook carefully we split the raw data into 2 part the first part is validation data
set, and the second part is the data set for the training purpose. We pick the data from the first row and 10th row from 
the `yes` on `went_on_backorder`, and we pick the first row from the `no`. As we could see we got the expected result, 
2 `yes` on the first two row and `no` on the last row. So we could say that our models trained correctly. It would
display the probability about how "sure" the model about predicting the result, the range is between 0 and 1. The
greater the number it says that the model is very "sure" about the prediction.

```
predict     No	            Yes
Yes         0.424039	    0.575961
Yes         0.0469849	    0.953015
No          0.983258	    0.0167421
```

## Deploying Data Model

If you remember we already export the MOJO model to be used on production system. We would like to use `java` as the
predictor as we already discussed earlier. We would like try to get h2o container id and copy the file into the running 
container by executing the command below:
```
cd model-predictor

export H2O_CONTAINER_ID=$(docker ps -aqf "name=h2o")
docker cp . $H2O_CONTAINER_ID:/root/h2o/h2o_venv/model-predictor
```

First we would like to load the model already defined earlier, the model placed one level up from the java
project, so we need to load it by using `..`. We would like to load the data that I already write based on the data 
validation on the Python code.
```
 EasyPredictModelWrapper model = new EasyPredictModelWrapper(MojoModel.load("../top_model.zip"));

    List<ReorderDataModel> reorderDataModelList = Arrays
      .asList(
        reorderDataModel1, reorderDataModel10, notReorderDataModel1
      );
```

After that we would like to iterate all the data by using `forEach`. 
```
    reorderDataModelList.forEach(reorderDataModel -> {
      RowData row = new RowData();
      row.put("sku", reorderDataModel.sku);
      ...

      BinomialModelPrediction p = null;

      try {
        p = model.predictBinomial(row);
      } catch (PredictException e) {
        e.printStackTrace();
      }

      System.out.println("User will reorder (1=yes; 0=no): " + p.label);
      System.out.print("Class probabilities: ");
      for (int i = 0; i < p.classProbabilities.length; i++) {
        if (i > 0) {
          System.out.print(",");
        }
        System.out.print(p.classProbabilities[i]);
      }
      System.out.println("");
    });
```

Now we would like to build and execute the java application that we already copy earlier into the docker container by
running the command below:
```
export H2O_CONTAINER_ID=$(docker ps -aqf "name=h2o")

docker exec -it $H2O_CONTAINER_ID bash

cd ~/h2o/h2o_venv/model-predictor
mvn package
mvn exec:java -Dexec.mainClass="com.example.Main"
```

You will see the output below:
```
User will reorder (1=yes; 0=no): Yes
Class probabilities: 0.4240393668884196,0.5759606331115804
User will reorder (1=yes; 0=no): Yes
Class probabilities: 0.046984898740402126,0.9530151012595979
User will reorder (1=yes; 0=no): No
Class probabilities: 0.9832578692993112,0.016742130700688782
```

Finally, we could train and deploy our model into java, and you could see the predicted result, and the probability is
absolutely the same between the model deployed on the python and deployed on java.

-----
Reference:
- https://docs.h2o.ai/h2o/latest-stable/h2o-docs/automl.html accessed at 6th August 2020.

---
##### Note to self:
```
docker build -t "adrian3ka/h2o:0.0.1" .

docker tag adrian3ka/h2o:0.0.1 adrian3ka/h2o:0.0.1
docker push adrian3ka/h2o:0.0.1
```
