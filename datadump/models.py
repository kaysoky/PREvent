from django.db import models
from django.contrib.auth.models import User

class Datapoint(models.Model):
    """
    Represents the collection of sensor data collected at a given time
    """

    # ID of the user
    userid = models.ForeignKey(User)

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
