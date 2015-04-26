from django.contrib import admin
from .models import Datapoint

# Admin permissions
admin.site.register(Datapoint)
