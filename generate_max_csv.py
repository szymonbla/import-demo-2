#!/usr/bin/env python3
import csv
import random
import os
from faker import Faker

fake = Faker()

def estimate_row_size():
    sample_row = {
        'id': 999999,
        'first_name': fake.first_name(),
        'last_name': fake.last_name(),
        'email': fake.email(),
        'phone': fake.phone_number(),
        'age': random.randint(18, 80),
        'city': fake.city(),
        'country': fake.country()
    }
    
    row_string = ','.join(str(value) for value in sample_row.values()) + '\n'
    return len(row_string.encode('utf-8'))

def generate_people_csv(filename='people_max.csv', target_size_mb=10):
    target_size_bytes = target_size_mb * 1024 * 1024
    
    avg_row_size = estimate_row_size()
    header_size = len('id,first_name,last_name,email,phone,age,city,country\n'.encode('utf-8'))
    
    estimated_rows = (target_size_bytes - header_size) // avg_row_size
    
    print(f"Estimated row size: {avg_row_size} bytes")
    print(f"Target file size: {target_size_mb}MB ({target_size_bytes} bytes)")
    print(f"Estimated rows to generate: {estimated_rows}")
    
    with open(filename, 'w', newline='', encoding='utf-8') as csvfile:
        fieldnames = ['id', 'first_name', 'last_name', 'email', 'phone', 'age', 'city', 'country']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        
        writer.writeheader()
        
        for i in range(1, estimated_rows + 1):
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
            
            if i % 10000 == 0:
                current_size = os.path.getsize(filename)
                print(f"Generated {i} rows, current size: {current_size / (1024*1024):.2f}MB")
                
                if current_size >= target_size_bytes:
                    print(f"Reached target size, stopping at {i} rows")
                    break
    
    final_size = os.path.getsize(filename)
    final_count = i
    print(f"Final: {final_count} people records in {filename}")
    print(f"Final file size: {final_size / (1024*1024):.2f}MB")
    
    return final_count, final_size

if __name__ == "__main__":
    generate_people_csv()
