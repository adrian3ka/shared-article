<h1>Prediction IO - Simple Model Development</h1>

<b>Level: Advance</b>

Prerequisites:
- [x] Naive Bayes
- [x] Scala
- [x] Java
- [x] Docker
- [x] Functional Programming
---
In this era we actually have a lot of problems that could be solved by
machine learning model. Almost every problem have the same development cycle.
In engineering development lifecycle we have a popular methodology to deliver our product to the user that called scrum.
Long story short scrum is a framework within which people can address complex adaptive problems, 
while productively and creatively delivering products of the highest possible value.

Building a machine learning model have its own pattern, here we go with the step:
- Problem: usually this part is a problem that came from the user to be solved by using automated program. The automated
  program could be hard-coded system, a rule engine, data-pipeline also with a model already derived from the
  analysis from the attribute that already provided by the data pipeline. Here we go our part as an engineer we should
  build the system properly in the right order (Be careful to be not over engineered in the early stage)
- Data Collection: As I told you before, we could only build a model if we already have a good pipeline first.
  For this chapter I would not explain to you how to build a good pipeline with its best practice. It would take another
  article to be explained well. So you can stay tune to have a complete understanding how to build a good machine learning 
  pipeline, and it's model. Basically this part is collecting data to be used for the modelling step.
- Modelling: This is a development part if you (as engineer) already decide that our system is well-prepared to be moved
  to this step. Usually data collection could be take up to 2 - 3 months to make sure it's already stable and well collected.
  So we could have a grasp about it (Never build a model we could not grasp from the early step). In this part we could
  elaborate with the data scientist or as an engineer you could define your own model by doing a black-box development system.
  Actually black-box is not a bad system, I will try to cover that in another article. A simple intermezzo about black-box is
  where you develop a model, that you really didn't need to know what's the calculation inside the box (system), but it will
  magically define a great output for you.
- Serving / Scoring: The goal of every machine learning model is predicting the result (include scoring, classifying, labelling, etc)
  the other data input with related / same attributes. Predicting the input parameters,
  actually include transforming into the right data type in example we use naive bayes as the model predictor
  you should convert enum type into the numeric (int / double) data type, so the predictor could handle it well.
  This method is called encoding, encoding could be differentiated into one hot encoding or data type encoding.
    - One hot encoding: Transform an inoperable data type in example enum. 
      This encoding is trying to transform the possibility of value in the datasets into binary mode. 
      In example there's a data type CustomerPreferenceEnum as on of an attribute in the model in context of E-Commerce industry.
      We want to build a model to predict the potential item(s) to be sold to current logged in user.
      The CustomerPreferenceEnum possibility consists of:
        - MUSIC
        - TECHNOLOGY
        - SPORT
        
      These are not gradable variable (maybe in some industries it would be gradable),
      so we should encode it using one hot encoding. The basic reasons behind it we couldn't say that MUSIC is higher than
     TECHNOLOGY or SPORT, well it's just a preference matter. 
    - Data type encoding: These attributes are gradable on its context. This encoding will encode the data into standard
      numeric type. In example, we have CustomerLoyaltyLevelEnum data type. The possibility consists of:
        - BASIC
        - SILVER
        - GOLD
        - PLATINUM
    
      We could encode it into BASIC (1.0), SILVER (2.0), GOLD (3.0), PLATINUM (4.0).

  So for the clarity I will try to give an example with customer attributes:
    - Customer Age Account `[INTEGER]`: 8 Months
    - Customer Salary `[INTEGER]`: 8.000.000
    - Customer Loyalty Level `[CustomerLoyaltyLevelEnum]`: SILVER
    - Customer Preference `[CustomerPreferenceEnum]`: TECHNOLOGY
  
  So from the data above we could encode it into JSON format to be:
  ```
  {
    "ageAccount": 8,
    "salary": 8000000,
    "loyaltyLevel": 2.0,
    "preferenceMusic": 0,
    "preferenceTechnology": 1,
    "preferenceSport": 0
  }
  ```
  As we could see that we encode the preference using binary mode, because it's not gradable.
- Evaluation: Every model should be evaluated to increase its performance and its accuracy. In this stage we try to analyze
  the current model. After that we will try to (re)modeling it to achieve the highest accuracy.
  
From the explanation above we could conclude that the pattern could be framed into the flowchart below:
```
  ___________         _________________         __________         ___________________
 |  Problem  | ----> | Data Collection | ----> | Modeling | ----> |     Predicting    |
  -----------         -----------------         ----------         -------------------
                                                    ^      ____________       |
                                                    |-----| Evaluation |<-----|
                                                           ------------
```
---
As we already found the development pattern, we could use apache prediction io as the development framework to be used 
for solving our current real life problem, because apache prediction io adapt to this pattern using the DASE pattern.
- `[D]` Data Source and Data Preparator
  Data Source reads data from an input source and transforms it into a desired format.
  Data Preparator preprocesses the data and forwards it to the algorithm for model training.
- `[A]` Algorithm
  The Algorithm component includes the Machine Learning algorithm, and the settings of its parameters,
  determines how a predictive model being constructed.
- `[S]` Serving
  The Serving component takes prediction queries and returns prediction results. 
  If the engine has multiple algorithms, Serving will combine the results into one. 
  Additionally, business-specific logic can be added in Serving to further customize the final returned results.
- `[E]` Evaluation Metrics
  An Evaluation Metric quantifies prediction accuracy with a numerical score. 
  It can be used for comparing algorithms or algorithm parameter settings.

In this example we try to build fraud detection model based on the data that we already 
collected from the seed data source. This case want to detect transfer scam. 
Maybe we could say that transfer scam refer to fraudsters that pose 
as a fake charity and contact unwitting victims,
persuading them to transfer money into the non-existent charity's bank account as a donation.

Before we proceed to the next step (I promise it's the last long tailored explanation) better to have a little grasp
about naive bayes, because we wanted to use it as our predictor model. 
Naive Bayes is a simple technique for constructing classifiers: models that assign class labels to problem instances, 
represented as vectors of feature values, where the class labels drawn from some finite set.

There is not a single algorithm for training such classifiers, 
but a family of algorithms based on a common principle: all naive Bayes classifiers assume that the value of a 
particular feature is independent of the value of any other feature, given the class variable. 
For example, a fruit may be considered to be an apple if it is red, round, and about 10 cm in diameter. 
A naive Bayes classifier considers each of these features to contribute independently to the 
probability that this fruit is an apple, regardless of any possible correlations between the color, roundness, 
and diameter features.

Going back to the main topic, out model consist of 5 parameters, or we can say it as 5 features. These are:
- TransactionVelocity `[transactionVelocity]`: How much the transaction occurs in the last session or window.
- GTV `[gtv]`: Sum all of any amount that occurs in the transaction whether its cash in or cash out.
- Total related account `[relatedAccount]`: In this case we could say the related account in the last window transaction.
- Account age in months `[accountAge]`: These features are indicate how long the account already resides in our platform.
- Card Type `[cardType]`: These features tell the card type from user whether <b>[SILVER/GOLD/PLATINUM]</b>

---
<h3>SDK</h3>
I will try to cover the Prediction IO SDK for Scala language, because currently PredictionIO only available in scala
language. Plus point if you already experienced with java programming languages, because the class / package we have 
in java, could be reusable in scala language.

---
<h3>Code Explanation</h3>

In PredictionIO, we should define files `engine.json` to define the algorithm that we want to use also its 
hyper-parameters, and the main engine class to be run. The other files required by this framework is `template.json`
file, it contains information about minimum pio version to run the specified model.

Okay let's start from the main class we already define in `engine.json`. In `FraudDetectionModelEngine` we should
override `apply` function that from the `EngineFactory` class.
```
def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("naive" -> classOf[NaiveBayesAlgorithm]),
      classOf[Serving])
  }
```
As we already view the component consists of:
- <b>DataSource</b>: The responsibilities of this class is to feed the data that already exists in the events' resource.
  Data Source reads data from the data store of Event Server and then Data Preparator prepares RDD[LabeledPoint] 
  for the Naive Bayes algorithm.
- <b>Preparator</b>: Data Preparator is where pre-processing actions occurs. 
  For example, one may want to remove some very popular items from the training data because she thinks that these items
  may not help to find an individual person's tastes or one may have a black list of item she wants to remove from the 
  training data before feeding it to the algorithm. As for the code the `prepare` method simply passes the ratings from TrainingData to PreparedData.
- <b>Map of Algorithm</b>: The available algorithm to be used by the engine. The `naive` came from `engine.json` file, where
  its resides on `algorithms` attributes. Every algorithm class, in example `NaiveBayesAlgorithm` class should override
  `train` and `predict` function. As we expected by its name they will do their job to train and predict the model, to have
  a good understanding behind the scene better you have to learn naive bayes algorithm 
  (you could find a simple explanation on the section above if you just now rush to this part)
- <b>Serving</b>: Serving component is where post-processing occurs. For example, if the user have a transaction greater 
  than 2 billion we wanted to return decision to the client as `SUSPICIOUS`. In this case you could combine it with your
  hardcoded rules or any other rules-engine that came from business or non engineer user. Trust me this case would be existed
  in real life example.

---
<h3>Model Training and Deployment</h3>

I know you are already wanted to jump to the main part, so you can start with starting docker that I already prepared. 
Just run the command below:
```
docker run -it -p 8001:8000 -p 7071:7070 adrian3ka/pio:0.0.2 /bin/bash
pio-start-all
```

Please wait 10 up to 30 seconds to let the engine warm up first. If you check it too early maybe you could get
some error message. To check whether the engine is ready to go you could do:
```
pio status
```

The expected result from the command above is:
```
[INFO] [Management$] Your system is all ready to go.
```

After the pio engine already started, we could start to importing the data. Before we proceed
it's better if we have a grasp about the data that would be imported. The data resides in the
`data/data.txt` file. Every row is representing 1 event, and it's result. In example:

```
FRAUDSTER,10 165000000 1 3 GOLD
```

From data above you can say the transaction with attribute:
- transactionVelocity `10`
- gtv `165000000`
- relatedAccount `1`
- accountAge `3`
- cardType `GOLD`

is a transaction that came from `FRAUDSTER` user.

Before we proceed into the sources code explanation and datasets import
we should prepare the environment first:
```
export FRAUD_MODEL_KEY=fraud-detection-model

pio app new FraudDetectionModel --access-key=${FRAUD_MODEL_KEY}

pio app list
```

Going back to the host (your computer) Folder on this directory. 
First of all we need to build the scala the jar with command below:
```
sbt package
```

Copy all the sources code including the Jar To Docker with command below:
```
docker container ls
export SPARK_CONTAINER_ID=bc07c00d3370
docker cp ./ ${SPARK_CONTAINER_ID}:/fraud-detection-model
```
To import all the data to the server we could run command below: 
```
cd fraud-detection-model

python data/import_eventserver.py --access_key $FRAUD_MODEL_KEY
```

To check whether the import is success, please do the command below:
You can get all the data by curl:
```
curl  -X GET "http://localhost:7070/events.json?accessKey=$FRAUD_MODEL_KEY&limit=-1" | jq
```

After we make sure all the events are fetch correctly we could,
move forward to the next step that's train and deploy the model.
You could simply deploy it by type the following command:
```
pio build --verbose
pio train
pio deploy
```

To check whether to engine built and trained correctly we could verify by our data train:

The curl below should return FRAUDSTER:
```
curl -H "Content-Type: application/json" -d \
'{ "transactionVelocity":10, "gtv":165000000, "relatedAccount":1, "accountAge": 3, "cardType": "GOLD" }'\
http://localhost:8001/queries.json
``` 

The curl below should return SAFE:
```
curl -H "Content-Type: application/json" -d \
'{ "transactionVelocity":1, "gtv":450000, "relatedAccount":1, "accountAge": 9, "cardType": "SILVER" }' \
http://localhost:8001/queries.json
``` 

The curl below should return SUSPICIOUS:
```
curl -H "Content-Type: application/json" -d \
'{ "transactionVelocity":4, "gtv":135000000, "relatedAccount":1, "accountAge": 96, "cardType": "PLATINUM" }' \
http://localhost:8001/queries.json
``` 

The curl below should return SUSPICIOUS, actually it's returning SAFE, 
but we add some hardcoded rules in `Serving` components
```
curl -H "Content-Type: application/json" -d \
'{ "transactionVelocity":6, "gtv":2000000001, "relatedAccount":6, "accountAge": 108, "cardType": "PLATINUM" }' \
http://localhost:8001/queries.json
``` 

---

So from the model above we could try some any other possibilities, you should feed more data to get a better result. 
So from here you can already deploy and built your own model. I think its already long enough for now, I will try to
cover unit test and evaluation part on the other article.

REFERENCES:
- https://www.scrum.org/resources/what-is-scrum accessed at 12 July 2020.
- https://predictionio.apache.org/ accessed at 12 July 2020.
- https://en.wikipedia.org/wiki/Naive_Bayes_classifier#:~:text=In%20statistics%2C%20Na%C3%AFve%20Bayes%20classifiers,the%20simplest%20Bayesian%20network%20models accessed at 12 July 2020.