from django.conf.urls import include, url
from django.contrib import admin
from rest_framework.urlpatterns import format_suffix_patterns
from datadump import views

urlpatterns = [
    url(r'^$', views.index),
    url(r'^api/', include('rest_framework.urls', namespace='rest_framework')),
    url(r'^accounts/', include('registration.backends.simple.urls')),
    url(r'^admin/', include(admin.site.urls)),
]

urlpatterns += format_suffix_patterns([
        url(r'^data/$', views.DatapointList.as_view())
    ])
