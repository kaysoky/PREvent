# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
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
            ],
        ),
        migrations.CreateModel(
            name='User',
            fields=[
                ('userid', models.IntegerField(serialize=False, primary_key=True)),
                ('username', models.CharField(max_length=100)),
                ('password', models.CharField(max_length=50)),
            ],
        ),
        migrations.AddField(
            model_name='datapoint',
            name='userid',
            field=models.ForeignKey(to='datadump.User'),
        ),
    ]
