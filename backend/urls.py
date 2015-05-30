from django.conf.urls import include, url
from django.contrib import admin
from rest_framework.urlpatterns import format_suffix_patterns
from registration.backends.simple.views import RegistrationView
from datadump import views

urlpatterns = [
    url(r'^$', views.index),
    url(r'^', include('rest_framework.urls', namespace='rest_framework')),
    url(r'^register/$', RegistrationView.as_view(), name='registration_register'),
    url(r'^register/complete/$', views.index, name='registration_complete'),
    url(r'^admin/', include(admin.site.urls)),
]

urlpatterns += format_suffix_patterns([
        url(r'^data/$', views.DatapointList.as_view()),
        url(r'^data/user/$', views.UserDatapointList.as_view()),
        url(r'^auth/$', views.AuthCheck.as_view()),
    ])
