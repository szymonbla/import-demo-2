#!/usr/bin/env python3
import csv
import random
from faker import Faker

fake = Faker()

def generate_people_csv(filename='people_small.csv', count=1000):
    with open(filename, 'w', newline='', encoding='utf-8') as csvfile:
        fieldnames = ['id', 'first_name', 'last_name', 'email', 'phone', 'age', 'city', 'country']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        
        writer.writeheader()
        
        for i in range(1, count + 1):
            writer.writerow({
                'id': i,
                'first_name': fake.first_name(),
                'last_name': fake.last_name(),
                'email': fake.email(),
                'phone': fake.phone_number(),
                'age': random.randint(18, 80),
                'city': fake.city(),
                'country': fake.country()
            })
    
    print(f"Generated {count} people records in {filename}")

if __name__ == "__main__":
    generate_people_csv()
