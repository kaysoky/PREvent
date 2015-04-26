from django.conf.urls import include, url
from django.contrib import admin
import datadump

urlpatterns = [
    # Examples:
    # url(r'^$', 'backend.views.home', name='home'),
    # url(r'^blog/', include('blog.urls')),

    url(r'^$', 'datadump.views.index'),
    url(r'^accounts/', include('registration.backends.simple.urls')),
    url(r'^admin/', include(admin.site.urls)),
]
