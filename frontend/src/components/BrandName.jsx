import React from 'react';
import { PRODUCT_NAME } from '../config/brand.js';

export default function BrandName() {
  return <span className="brand-name" aria-label={PRODUCT_NAME}><span aria-hidden="true">cram<span className="brand-name-w">W</span>me</span></span>;
}
