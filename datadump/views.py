from django.shortcuts import render
from django.http import HttpResponse
from django.shortcuts import render

from rest_framework import generics
from rest_framework.permissions import *

from models import Datapoint
from serializers import DatapointSerializer

def index(request):
    return render(request, 'index.html')

from rest_framework import generics


class DatapointList(generics.ListCreateAPIView):
    """ List all datapoints, or create a new datapoint """
    permission_classes = (IsAuthenticatedOrReadOnly,)
    
    queryset = Datapoint.objects.all()
    serializer_class = DatapointSerializer
    
    def perform_create(self, serializer):
        serializer.save(userid=self.request.user)
