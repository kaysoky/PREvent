import argparse
import base64
from datetime import datetime, timedelta
import json
import random
import requests

if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='Spams the PREvent backend with test data')
    parser.add_argument('--endpoint', 
        type=str, default='http://localhost:8000/data/',
        help='POST target')
    parser.add_argument('--n', 
        type=int, default=100,
        help='Number of datapoints to generate')
    parser.add_argument('--username', 
        type=str, default='admin', 
        help='Username of the POST-er')
    parser.add_argument('--password', 
        type=str, default='password', 
        help='Password of the POST-er')
    parser.add_argument('--latitude', 
        type=float, default=47.7, 
        help='Average latitude of the datapoints')
    parser.add_argument('--longitude', 
        type=float, default=-122.3, 
        help='Average longitude of the datapoints')
    parser.add_argument('--geospread', 
        type=float, default=5.0, 
        help='Amount of deviation in GPS coordinates')
    args = parser.parse_args()
    
    # Timestamp the data from now and backwards by 1 minute increments
    now = datetime.now()
    delta = timedelta(minutes=1)
    
    for i in range(args.n):
        datapoint = {
            "timestamp": (now - delta * i).strftime('%Y-%m-%dT%H:%M'),
            "xcoord": (2 * (random.random() - 0.5) * args.geospread) + args.longitude,
            "ycoord": (2 * (random.random() - 0.5) * args.geospread) + args.latitude,
            "humidity": random.random(),
            "temperature": random.random(),
            "gas": random.random(),
            "particulate": random.random()
        }
        requests.post(args.endpoint, data=json.dumps(datapoint), 
            headers={'Content-Type': 'application/json', 
                     'Authorization': 'Basic %s' % base64.b64encode('%s:%s' % (args.username, args.password))})
