import django_filters
from django.shortcuts import render
from django.http import HttpResponse
from django.shortcuts import render

from rest_framework import generics
from rest_framework.permissions import IsAuthenticatedOrReadOnly, AllowAny
from rest_framework.pagination import LimitOffsetPagination

from models import Datapoint
from serializers import DatapointSerializer


def index(request):
    return render(request, 'index.html')
    
class DatapointFilter(django_filters.FilterSet):
    """ Filters that can be applied to a GET's query string """
    xmin = django_filters.NumberFilter(name='xcoord', lookup_type='gte')
    xmax = django_filters.NumberFilter(name='xcoord', lookup_type='lte')
    ymin = django_filters.NumberFilter(name='ycoord', lookup_type='gte')
    ymax = django_filters.NumberFilter(name='ycoord', lookup_type='lte')
    before = django_filters.DateTimeFilter(name='timestamp', lookup_type='lte')
    after = django_filters.DateTimeFilter(name='timestamp', lookup_type='gte')
    
    class Meta:
        model = Datapoint
        fields = ['xmin', 'xmax', 'ymin', 'ymax', 'before', 'after']

class DatapointList(generics.ListCreateAPIView):
    """ List all datapoints, or create a new datapoint """
    permission_classes = (IsAuthenticatedOrReadOnly,)
    
    queryset = Datapoint.objects.all()
    serializer_class = DatapointSerializer
    filter_class = DatapointFilter
    
    def perform_create(self, serializer):
        serializer.save(userid=self.request.user)

class UserDatapointList(generics.ListAPIView):
    """ List all datapoints belonging to the given user """
    permission_classes = (AllowAny,)
    serializer_class = DatapointSerializer
    filter_class = DatapointFilter
    
    def get_queryset(self):
        return Datapoint.objects.filter(userid=self.request.user)
