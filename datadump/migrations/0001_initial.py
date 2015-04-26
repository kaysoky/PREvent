# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
from django.conf import settings


class Migration(migrations.Migration):

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name='Datapoint',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('timestamp', models.DateTimeField()),
                ('xcoord', models.FloatField()),
                ('ycoord', models.FloatField()),
                ('humidity', models.FloatField()),
                ('temperature', models.FloatField()),
                ('gas', models.FloatField()),
                ('particulate', models.FloatField()),
                ('userid', models.ForeignKey(to=settings.AUTH_USER_MODEL)),
            ],
        ),
    ]
