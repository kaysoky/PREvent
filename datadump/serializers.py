from django.forms import widgets
from django.contrib.auth.models import User
from rest_framework import serializers
from datadump.models import Datapoint

class DatapointSerializer(serializers.ModelSerializer):
    userid = serializers.ReadOnlyField(source='userid.username')

    class Meta:
        model = Datapoint
