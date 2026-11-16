import React from 'react';

export type ButtonProps = {
  variant?: 'primary' | 'secondary';
  size?: 'sm' | 'md' | 'lg';
  disabled?: boolean;
  icon?: React.ReactNode;
  children: React.ReactNode;
  onClick?: () => void;
};

export const Button: React.FC<ButtonProps> = ({
  variant = 'primary',
  size = 'md',
  disabled = false,
  icon,
  children,
  onClick,
}) => {
  const className = `btn ${variant} ${size}`;
  return (
    <button className={className} disabled={disabled} onClick={onClick}>
      {icon && <span className="icon">{icon}</span>}
      {children}
    </button>
  );
};

