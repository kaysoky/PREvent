from django.db import models

class User(models.Model):
    """
    Represents a single user of this service
    """

    userid = models.IntegerField(primary_key=True)

    username = models.CharField(max_length=100)
    password = models.CharField(max_length=50)

class Datapoint(models.Model):
    """
    Represents the collection of sensor data collected at a given time
    """

    # ID of the user
    userid = models.ForeignKey('User')

    # Time of data collection, not the time of storage
    timestamp = models.DateTimeField()

    # GPS coordinates
    xcoord = models.FloatField()
    ycoord = models.FloatField()

    # Data from the sensors
    humidity = models.FloatField()
    temperature = models.FloatField()
    gas = models.FloatField()
    particulate = models.FloatField()
